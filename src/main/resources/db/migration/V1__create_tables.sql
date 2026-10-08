CREATE TABLE tb_dono(
    id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE tb_veterinario(
    id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    crm VARCHAR(20) NOT NULL UNIQUE,
    especialidade VARCHAR(100),
    email VARCHAR(150) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE tb_atendente(
    id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE tb_pet(
    id SERIAL PRIMARY KEY,
    nome_pet VARCHAR(100) NOT NULL,
    idade_pet INTEGER,
    raca_pet VARCHAR(50),
    tipo_pet VARCHAR(20) NOT NULL,
    dono_id INTEGER     NOT NULL,
    CONSTRAINT fk_pet_dono FOREIGN KEY (dono_id) REFERENCES tb_dono (id)
);

