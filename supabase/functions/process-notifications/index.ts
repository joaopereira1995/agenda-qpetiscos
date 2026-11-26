import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";
import { SignJWT, importPKCS8 } from "npm:jose@5";
Deno.serve(async (req) => {
 const secret=Deno.env.get("NOTIFICATION_WORKER_SECRET");
 if(req.method!=="POST")return Response.json({error:"Method not allowed"},{status:405});
 if(!secret||req.headers.get("x-worker-secret")!==secret)return Response.json({error:"Unauthorized"},{status:401});
 try {
 const db=createClient(Deno.env.get("SUPABASE_URL")!,Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
 const rpc=async(name:string,args?:Record<string,unknown>)=>{const {data,error}=await db.rpc(name,args);if(error)throw error;return data};
 const generated:Record<string,unknown>={events:await rpc("generate_event_notifications"),reminders:await rpc("generate_reminder_notifications"),daily_summaries:await rpc("generate_daily_summary_notifications")};
 // A failure in the new generators must not stop existing event deliveries.
 for(const name of ["generate_internal_note_notifications","generate_task_reminder_notifications"]){
   try{generated[name]=await rpc(name)}catch(_error){generated[name]="failed";console.error(name+" failed")}
 }
 const account=JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT")!);
 const key=await importPKCS8(account.private_key,"RS256");
 const assertion=await new SignJWT({scope:"https://www.googleapis.com/auth/firebase.messaging"}).setProtectedHeader({alg:"RS256"}).setIssuer(account.client_email).setAudience("https://oauth2.googleapis.com/token").setIssuedAt().setExpirationTime("1h").sign(key);
 const oauth=await fetch("https://oauth2.googleapis.com/token",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:new URLSearchParams({grant_type:"urn:ietf:params:oauth:grant-type:jwt-bearer",assertion}),signal:AbortSignal.timeout(10000)});
 const auth=await oauth.json();if(!oauth.ok||!auth.access_token)throw Error("Firebase authentication failed");
 const deliveries=await rpc("claim_notification_deliveries",{p_limit:10});
 const stats={processed:0,sent:0,failed:0,skipped:0};
 for(const d of deliveries??[]){stats.processed++;
 const update=async(values:Record<string,unknown>)=>{const {error}=await db.from("notification_deliveries").update({...values,updated_at:new Date().toISOString()}).eq("id",d.id).eq("status","processing").eq("attempts",d.attempts).eq("processing_started_at",d.processing_started_at).select("id").single();if(error)throw error;};
 try {
 const {data:device,error:de}=await db.from("devices").select("*").eq("id",d.device_id).single();if(de)throw de;
 const {data:n,error:ne}=await db.from("notifications").select("*").eq("id",d.notification_id).eq("device_id",d.device_id).single();if(ne)throw ne;
 const skip=async(reason:string)=>{await update({status:"skipped",next_attempt_at:null,last_error:reason});stats.skipped++};
 if(!device.notifications_enabled||!device.fcm_token||n.dismissed_at){await skip("Disabled, dismissed or missing token");continue;}
 const isNote=String(n.type).startsWith("note_")||String(n.type).startsWith("task_");
 if(isNote){
   const {data:p,error:pe}=await db.from("notification_preferences").select("notes_notifications_enabled").eq("device_id",device.id).maybeSingle();if(pe)throw pe;
   if(!p?.notes_notifications_enabled){await skip("Note notifications disabled");continue;}
   if(n.type==="task_reminder"&&await rpc("validate_task_reminder_notification",{p_notification_id:n.id})!==true){await skip("Task reminder no longer valid");continue;}
 }
 if(n.type==="reminder"&&await rpc("validate_reminder_notification",{p_notification_id:n.id})!==true){await skip("Reminder no longer valid");continue;}
 if(n.type==="daily_summary"&&n.details?.schedule_revision!=null){const {data:p,error:pe}=await db.from("notification_preferences").select("summary_schedule_revision,daily_summary_enabled").eq("device_id",device.id).maybeSingle();if(pe)throw pe;if(p&&(p.daily_summary_enabled===false||String(p.summary_schedule_revision)!==String(n.details.schedule_revision))){await skip("Summary rescheduled or disabled");continue;}}
 const data:Record<string,string>={notification_id:String(n.id),event_id:String(n.event_id??""),type:String(n.type)};
 const custom=device.push_protocol===2;
 const limit=(v:unknown,max:number)=>Array.from(String(v??"")).slice(0,max).join("");
 if(custom){data.title=limit(n.title,100);data.body=limit(n.body,650);const details=n.details??{};for(const [k,max] of Object.entries({name:100,kind:20,date:20,time:10,people:12,notes:240,source:60,reminder_minutes:10,reservations:12,takeaways:12,lunch:12,dinner:12}))data[k]=limit(details[k],max);if(isNote){data.note_id=limit(details.note_id,40);data.assigned=limit(details.assigned,60);data.important=limit(details.important,5);}}
 if(custom){const encoder=new TextEncoder();while(encoder.encode(JSON.stringify(data)).length>3000){const k=data.notes.length?"notes":"body";if(!data[k].length)break;data[k]=Array.from(data[k]).slice(0,-30).join("");}}
 const message=custom?{token:device.fcm_token,data,android:{priority:"high",ttl:n.type==="reminder"||n.type==="task_reminder"?"60s":"86400s"}}:{token:device.fcm_token,data,notification:{title:n.title,body:n.body},android:{priority:"high",notification:{channel_id:"qpetiscos_notifications",tag:n.id,sound:"default"}}};
 const response=await fetch(`https://fcm.googleapis.com/v1/projects/${account.project_id}/messages:send`,{method:"POST",headers:{Authorization:`Bearer ${auth.access_token}`,"Content-Type":"application/json"},body:JSON.stringify({message}),signal:AbortSignal.timeout(10000)});
 const result=await response.json();if(!response.ok){if(result.error?.details?.some((x:{errorCode?:string})=>x.errorCode==="UNREGISTERED")){const {error}=await db.from("devices").update({fcm_token:null,push_protocol:1}).eq("id",device.id).eq("fcm_token",device.fcm_token);if(error)throw error;await skip("Invalid Firebase token");continue;}throw Error(`FCM HTTP ${response.status}`);}
 await update({status:"sent",sent_at:new Date().toISOString(),firebase_message_id:result.name??null,next_attempt_at:null,last_error:null});stats.sent++;
 }catch(_error){await update({status:"failed",next_attempt_at:d.attempts<3?new Date(Date.now()+(d.attempts===1?1:5)*60000).toISOString():null,last_error:"Delivery processing failed"});stats.failed++;}
 }
 return Response.json({success:true,worker_authenticated:true,firebase_connected:true,generated,...stats});
 }catch(_error){console.error("Notification worker failed");return Response.json({success:false,error:"Notification worker failed"},{status:500});}
});
