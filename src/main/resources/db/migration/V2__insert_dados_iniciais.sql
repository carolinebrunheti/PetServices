INSERT INTO tb_dono (nome, email, senha, role) VALUES
    ('Carlos Souza', 'carlos@email.com', '123456', 'TUTOR'),
    ('Mariana Lima', 'mariana@email.com', '123456', 'TUTOR');

INSERT INTO tb_veterinario (nome, crm, especialidade, email, senha, role) VALUES
    ('Dra. Ana Ribeiro', 'CRMV-SP 12345', 'Clínica Geral', 'ana@petservices.com', '123456', 'VETERINARIO'),
    ('Dr. Pedro Alves', 'CRMV-SP 67890', 'Ortopedia', 'pedro@petservices.com', '123456', 'VETERINARIO');

INSERT INTO tb_atendente (nome, email, senha, role) VALUES
    ('Julia Costa', 'julia@petservices.com', '123456', 'ATENDENTE');

INSERT INTO tb_pet (nome_pet, idade_pet, raca_pet, tipo_pet, dono_id) VALUES
    ('Thor', 6, 'Labrador', 'CACHORRO', (SELECT id FROM tb_dono WHERE email = 'carlos@email.com')),
    ('Mel', 4, 'Siamês', 'GATO', (SELECT id FROM tb_dono WHERE email = 'carlos@email.com')),
    ('Bob', 6, 'Vira-lata', 'CACHORRO', (SELECT id FROM tb_dono WHERE email = 'mariana@email.com'));
