FROM confluentinc/cp-kafka-connect:7.6.1

# Keep plugin path explicit and stable across environments.
ENV CONNECT_PLUGIN_PATH=/usr/share/java,/usr/share/confluent-hub-components,/etc/kafka-connect/plugins

# Install Confluent S3 Sink Connector (compatible with CP 7.6.1)
RUN confluent-hub install --no-prompt confluentinc/kafka-connect-s3:10.5.13

# Place custom connector jars under docker/kafka-connect/plugins/.
COPY docker/kafka-connect/plugins/ /etc/kafka-connect/plugins/
