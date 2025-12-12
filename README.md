# E-Commerce Inventory & Dynamic Pricing API

A comprehensive backend API for managing e-commerce inventory, dynamic pricing, shopping carts, and checkout processes. Built with Spring Boot, this API supports product management, stock tracking, flexible pricing rules, and secure user authentication.

## Features

- **Product Management**: Create, update, and manage products with variants (SKUs)
- **Inventory Control**: Track stock levels and handle reservations for concurrent purchases
- **Dynamic Pricing**: Flexible pricing engine with rules for discounts, promotions, and user tiers
- **Shopping Cart**: Persistent cart management with automatic expiration
- **Secure Checkout**: Thread-safe checkout process with inventory reservation
- **Category Management**: Hierarchical product categorization
- **User Authentication**: JWT-based authentication and authorization
- **API Documentation**: OpenAPI/Swagger documentation
- **Database Integration**: PostgreSQL for data persistence, Redis for caching

## Tech Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Security**: Spring Security with JWT
- **API Documentation**: SpringDoc OpenAPI
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker & Docker Compose
- Git

## Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd E-commerce-Inventory-and-Dynamic-Pricing-API--24A95A1211
   ```

2. **Start the services using Docker Compose**
   ```bash
   docker-compose up -d
   ```
   This will start:
   - PostgreSQL database on port 5432
   - Redis cache on port 6379
   - Spring Boot application on port 8080

3. **Initialize the database**
   ```bash
   # Run the schema script
   docker-compose exec postgres psql -U ecommerce -d ecommerce -f /docker-entrypoint-initdb.d/db-schema.sql

   # Run the seed data script
   docker-compose exec postgres psql -U ecommerce -d ecommerce -f /docker-entrypoint-initdb.d/seed-data.sql
   ```

4. **Access the application**
   - API Base URL: http://localhost:8080
   - API Documentation: http://localhost:8080/swagger-ui.html
   - Health Check: http://localhost:8080/actuator/health

## Manual Setup (Without Docker)

1. **Install dependencies**
   ```bash
   mvn clean install
   ```

2. **Set up PostgreSQL and Redis**
   - Install PostgreSQL 15 and create database `ecommerce`
   - Install Redis 7
   - Update `src/main/resources/application.properties` with your database credentials

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

## API Endpoints

### Products
- `GET /products` - List all products
- `POST /products` - Create a new product
- `GET /products/{id}` - Get product details
- `PUT /products/{id}` - Update product
- `DELETE /products/{id}` - Archive product

### Variants (SKUs)
- `POST /products/{productId}/variants` - Add variant to product
- `GET /variants/{id}` - Get variant details
- `PATCH /variants/{id}` - Update variant stock/price

### Categories
- `GET /categories` - List all categories (hierarchical)
- `POST /categories` - Create new category

### Pricing
- `GET /products/{productId}/price` - Calculate dynamic price with rules

### Cart Management
- `POST /cart` - Create/get active cart
- `POST /cart/items` - Add item to cart
- `PATCH /cart/items/{id}` - Update cart item quantity
- `DELETE /cart/items/{id}` - Remove item from cart
- `POST /cart/checkout` - Checkout cart

### Authentication
- `POST /auth/login` - User login
- `POST /auth/register` - User registration

For detailed API specifications, see [openapi.yaml](openapi.yaml).

## Database Schema

The database schema is defined in [db-schema.sql](db-schema.sql). Key entities include:

- `products` - Product catalog
- `variants` - Product variants with stock and pricing
- `categories` - Hierarchical product categories
- `pricing_rules` - Dynamic pricing rules
- `carts` & `cart_items` - Shopping cart functionality
- `orders` - Order management
- `reservations` - Inventory reservations
- `users` - User accounts

## Testing

Run the test suite:
```bash
mvn test
```

Key tests include:
- Authentication controller tests
- Checkout service concurrency tests

## Configuration

Key configuration properties in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce
spring.datasource.username=ecommerce
spring.datasource.password=ecommerce

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT
jwt.secret=your-secret-key
jwt.expiration=86400000

# Application
server.port=8080
```

## Development

### Project Structure
```
src/
├── main/java/com/example/ecommerce/
│   ├── config/          # Security and configuration
│   ├── controller/      # REST API endpoints
│   ├── entity/          # JPA entities
│   ├── repository/      # Data access layer
│   ├── service/         # Business logic
│   └── worker/          # Background workers
└── test/                # Unit and integration tests
```

### Key Components

- **PricingEngine**: Handles dynamic pricing calculations
- **CheckoutService**: Manages cart checkout with concurrency control
- **ReservationExpiryWorker**: Cleans up expired inventory reservations
- **JwtService**: JWT token management
- **CartService**: Cart operations and management

## Deployment

### Docker Deployment
```bash
# Build and run
docker-compose up --build -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

### Production Considerations
- Configure production database credentials
- Set secure JWT secret
- Enable HTTPS
- Configure logging and monitoring
- Set up database backups
- Consider horizontal scaling for high traffic

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

