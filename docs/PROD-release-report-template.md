# Relatório individual de lançamento PROD

Preencher com evidência real; não marcar validações não executadas como aprovadas.

| Campo | Evidência |
|---|---|
| Versão / build candidata | |
| SHA develop / SHA main anterior | |
| Validação TEST e aprovação do proprietário | |
| Takeoff: diffs, conflitos, testes, isolamento, CI/CD | |
| Decisão GO / NO-GO e bloqueadores | |
| Salvaguarda: versão anterior, branch, tag, SHA, release e APK | |
| Supabase: necessidade de migração, compatibilidade, backup, confirmação do proprietário | |
| Unique Test: SHA main resultante, run ID, artefacto, assinatura, SHA-256 | |
| Aceitação individual: resultados, data e aprovação explícita | |
| Go Live: tag, release, digest verificado, data, distribuição | |
| Plano de rollback, riscos e responsáveis | |

## Gates
- [ ] TEST aprovado e Takeoff GO.
- [ ] Branch e tag de segurança verificadas no SHA anterior.
- [ ] Backup/migrações PROD confirmados pelo proprietário, se necessários.
- [ ] Compilação PROD concluída sem release.
- [ ] APK individual aprovado pelo proprietário, hash registado.
- [ ] Go Live autorizado; bytes do APK anexado verificados antes da publicação.
- [ ] Release publicada e canal estável verificado.

Qualquer alteração ao candidato, refs auditadas ou APK exige revalidação. Não executar SQL PROD autonomamente. Não reconstruir o APK aprovado. Não substituir artefactos publicados.
