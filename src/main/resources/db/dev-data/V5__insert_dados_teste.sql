INSERT INTO tb_dono (nome, email, senha, role) VALUES
    ('Fernanda Rocha', 'fernanda@email.com', '123456', 'TUTOR'),
    ('Ricardo Mendes', 'ricardo@email.com', '123456', 'TUTOR'),
    ('Beatriz Santos', 'beatriz@email.com', '123456', 'TUTOR'),
    ('Lucas Oliveira', 'lucas@email.com', '123456', 'TUTOR');

INSERT INTO tb_veterinario (nome, crm, especialidade, email, senha, role) VALUES
    ('Dra. Camila Torres', 'CRMV-SP 11223', 'Dermatologia', 'camila@petservices.com', '123456', 'VETERINARIO'),
    ('Dr. Rafael Nunes', 'CRMV-SP 44556', 'Cardiologia', 'rafael@petservices.com', '123456', 'VETERINARIO'),
    ('Dra. Larissa Prado', 'CRMV-SP 77889', 'Animais Silvestres', 'larissa@petservices.com', '123456', 'VETERINARIO'),
    ('Dr. Gustavo Freitas', 'CRMV-SP 99001', 'Odontologia', 'gustavo@petservices.com', '123456', 'VETERINARIO');

INSERT INTO tb_atendente (nome, email, senha, role) VALUES
    ('Paula Martins', 'paula@petservices.com', '123456', 'ATENDENTE'),
    ('Diego Barbosa', 'diego@petservices.com', '123456', 'ATENDENTE'),
    ('Renata Cardoso', 'renata@petservices.com', '123456', 'ATENDENTE'),
    ('Thiago Moreira', 'thiago@petservices.com', '123456', 'ATENDENTE');

INSERT INTO tb_admin (nome, email, senha, role) VALUES
    ('Admin Principal', 'admin@petservices.com', '123456', 'ADMIN'),
    ('Sofia Almeida', 'sofia@petservices.com', '123456', 'ADMIN'),
    ('Bruno Teixeira', 'bruno@petservices.com', '123456', 'ADMIN'),
    ('Helena Duarte', 'helena@petservices.com', '123456', 'ADMIN');

INSERT INTO tb_pet (nome, data_nascimento, raca, especie, dono_id) VALUES
    ('Luna', '2021-03-15', 'Golden Retriever', 'CACHORRO', (SELECT id FROM tb_dono WHERE email = 'fernanda@email.com')),
    ('Nina', '2022-07-08', 'Persa', 'GATO', (SELECT id FROM tb_dono WHERE email = 'ricardo@email.com')),
    ('Kiwi', '2023-01-20', 'Calopsita', 'AVE', (SELECT id FROM tb_dono WHERE email = 'beatriz@email.com')),
    ('Pipoca', '2020-11-02', 'Hamster Sírio', 'OUTRO', (SELECT id FROM tb_dono WHERE email = 'lucas@email.com')),
    ('Max', '2019-05-30', 'Pastor Alemão', 'CACHORRO', (SELECT id FROM tb_dono WHERE email = 'fernanda@email.com'));
