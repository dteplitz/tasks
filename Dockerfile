# Use the OpenSearch base image
FROM opensearchproject/opensearch:2.14.0

# Switch to root user to perform setup
USER root

ENV discovery.type=single-node
ENV OPENSEARCH_JAVA_OPTS="-Xms512m -Xmx512m"
ENV DISABLE_SECURITY_PLUGIN=true

# Create the plugins directory if it doesn't exist and another directory to place the zip file
RUN mkdir -p /usr/share/opensearch/external-plugins

# Copy the plugin ZIP file into the Docker image with correct ownership
COPY --chown=opensearch:opensearch build/distributions/tasks.zip /usr/share/opensearch/external-plugins/

# Ensure correct permissions for the plugin ZIP file
RUN chmod 644 /usr/share/opensearch/external-plugins/tasks.zip

# Debug: List the contents of the external-plugins directory to verify the ZIP file is there
RUN ls -la /usr/share/opensearch/external-plugins/

# Debug: Check if the plugin ZIP file exists
RUN if [ -f /usr/share/opensearch/external-plugins/tasks.zip ]; then echo "Plugin ZIP file found"; else echo "Plugin ZIP file not found"; exit 1; fi

# Switch to opensearch user to install the plugin
USER opensearch

# Install the plugin from the external-plugins directory
RUN /usr/share/opensearch/bin/opensearch-plugin install file:///usr/share/opensearch/external-plugins/tasks.zip || (echo "Plugin installation failed"; exit 1)

# Expose ports
EXPOSE 9200 9600

# Switch back to root user for any further configuration
USER root

# Debug: Capture the OpenSearch logs
RUN cat /usr/share/opensearch/logs/opensearch.log || echo "Log file not found"

# Ensure correct permissions for the plugin directory
RUN chown -R opensearch:opensearch /usr/share/opensearch/plugins/tasks

# Switch back to the opensearch user
USER opensearch
