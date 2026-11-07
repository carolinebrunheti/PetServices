-- Padrao Transactional Outbox: o evento e gravado nesta tabela NA MESMA transacao
-- do cadastro. O OutboxPublisher le os pendentes (enviado_em IS NULL) e publica no Kafka.
-- Se o Kafka estiver fora, os eventos esperam aqui em vez de se perderem.
CREATE TABLE tb_outbox_evento (
    id          BIGSERIAL PRIMARY KEY,
    topico      VARCHAR(100) NOT NULL,
    chave       VARCHAR(100) NOT NULL,
    tipo        VARCHAR(200) NOT NULL,   -- classe do evento, para desserializar o payload
    payload     TEXT         NOT NULL,   -- evento em JSON
    criado_em   TIMESTAMP    NOT NULL,
    enviado_em  TIMESTAMP,
    tentativas  INTEGER      NOT NULL DEFAULT 0,
    ultimo_erro VARCHAR(500)
);

-- indice parcial: so os pendentes, que e o que o publicador consulta a cada segundo
CREATE INDEX idx_outbox_pendentes ON tb_outbox_evento (id) WHERE enviado_em IS NULL;
