-- as senhas dos dados de exemplo (V2 e V5) estavam em texto puro.
-- a senha continua sendo 123456, mas agora guardada como hash BCrypt
UPDATE tb_admin SET senha = '$2a$10$rS2V4XsYBzV.AAdLWECNFOPR/AjjOGfmG3ywF.m0DP46hA1q86Fuq' WHERE senha = '123456';
UPDATE tb_atendente SET senha = '$2a$10$rS2V4XsYBzV.AAdLWECNFOPR/AjjOGfmG3ywF.m0DP46hA1q86Fuq' WHERE senha = '123456';
UPDATE tb_veterinario SET senha = '$2a$10$rS2V4XsYBzV.AAdLWECNFOPR/AjjOGfmG3ywF.m0DP46hA1q86Fuq' WHERE senha = '123456';
UPDATE tb_dono SET senha = '$2a$10$rS2V4XsYBzV.AAdLWECNFOPR/AjjOGfmG3ywF.m0DP46hA1q86Fuq' WHERE senha = '123456';
