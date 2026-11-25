# Agenda QPetiscos 0.6.4

Atualização de código-fonte baseada no repositório 0.6.3. Substituir os ficheiros no repositório mantendo a pasta .github, publicar GitHub Pages e executar o workflow Compilar Android. Manter a assinatura existente. Não é necessário Android Studio.

Inclui cabeçalho com logótipo original, central de notificações, leitura individual e global, lembretes, preferências, resumo diário e integração FCM Android. O sino consulta o servidor a cada 15 segundos enquanto a aplicação está visível. O toque numa notificação Android abre a central. O formulário preserva o destinatário original de um lembrete individual ao editar um evento.

## Validação necessária
- Confirmar que a página abre e que os widgets, temas e atualizações continuam funcionais.
- Instalar o APK assinado em dois dispositivos, iniciar sessão com a mesma conta, atribuir nomes diferentes e autorizar notificações.
- Confirmar tokens FCM distintos na tabela devices (não partilhar os tokens).
- Criar reserva num dispositivo e verificar aviso no outro, incluindo com a aplicação em segundo plano; testar edição, eliminação, leitura e contador.
- Guardar preferências diferentes e confirmar persistência; testar lembrete de 5 minutos e resumo diário.
- Confirmar políticas RLS de leitura/atualização de notifications e leitura/inserção/atualização de notification_preferences para dispositivos da conta autenticada. A conta partilhada não constitui isolamento de segurança entre dispositivos.

Validação local: sintaxe JavaScript e XML. Compilação Android e testes reais não executados neste ambiente. O código depende das funções Supabase e do cron já instalados. O envio FCM pode atrasar e não garante entrega única. A atualização de token é efetuada ao abrir/retomar a aplicação; se o token mudar enquanto fechada, será atualizado na próxima abertura. A configuração Firebase cliente não contém a chave privada de serviço.
