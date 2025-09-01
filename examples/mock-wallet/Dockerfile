# Mock wallet Dockerfile to copy files instead of using volume mounts
FROM nginx:alpine

# Copy HTML files to nginx document root
COPY index.html /usr/share/nginx/html/

# Expose port 80
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]