-- V4__ajusta_colunas_pet.sql
ALTER TABLE tb_pet RENAME COLUMN nome_pet TO nome;
ALTER TABLE tb_pet RENAME COLUMN raca_pet TO raca;
ALTER TABLE tb_pet RENAME COLUMN tipo_pet TO especie;
ALTER TABLE tb_pet DROP COLUMN idade_pet;
ALTER TABLE tb_pet ADD COLUMN data_nascimento DATE;