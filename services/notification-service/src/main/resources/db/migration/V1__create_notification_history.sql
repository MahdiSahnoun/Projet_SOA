CREATE TABLE notification_history (
    id UUID PRIMARY KEY,
    event_id VARCHAR(128),
    notification_type VARCHAR(40),
    recipient_email VARCHAR(320),
    reference_value VARCHAR(512),
    delivery_status VARCHAR(16) NOT NULL,
    attempt_count INTEGER NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_summary VARCHAR(256),
    kafka_topic VARCHAR(249) NOT NULL,
    kafka_partition INTEGER NOT NULL,
    kafka_offset BIGINT NOT NULL,
    CONSTRAINT uq_notification_history_kafka_record
        UNIQUE (kafka_topic, kafka_partition, kafka_offset)
);

CREATE INDEX idx_notification_history_received_at
    ON notification_history (received_at DESC);
