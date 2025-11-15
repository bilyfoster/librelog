FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy source code
COPY . .

# Create non-root user but keep root for development (permission issues)
RUN addgroup -g 1001 -S nodejs
RUN adduser -S nextjs -u 1001 || true

# Change ownership
RUN chown -R nextjs:nodejs /app || true
# Use root user for development to avoid permission issues
# USER nextjs

# Expose port
EXPOSE 3000

# Default command
CMD ["npm", "run", "dev"]
