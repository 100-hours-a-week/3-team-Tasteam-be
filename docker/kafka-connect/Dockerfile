FROM confluentinc/cp-kafka-connect:7.6.1

# Keep plugin path explicit and stable across environments.
ENV CONNECT_PLUGIN_PATH=/usr/share/java,/usr/share/confluent-hub-components,/etc/kafka-connect/plugins

# Place custom connector jars under docker/kafka-connect/plugins/.
COPY docker/kafka-connect/plugins/ /etc/kafka-connect/plugins/
