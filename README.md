```
docker compose up -d
docker-compose down -v

docker exec -it b2b_mongodb_dev mongosh -u admin -p secret --authenticationDatabase admin --eval '
db.getSiblingDB("orders_db").partners.insertOne({
  name: "Empresa Parceira Teste LTDA",
  cnpj: "12.345.678/0001-90",
  creditLimit: 10000.00,
  availableLimit: 10000.00,
  createdAt: new ISODate(),
  updatedAt: new ISODate()
});
'

curl -X POST http://localhost:8080/api/v1/orders   -H "Content-Type: application/json"   -d '{
    "partnerId": "6a929a3b56fca84960c868bb",
    "items": [
      {
        "productId": "PROD-001",
        "quantity": 2,
        "unitPrice": 150.50
      },
      {
        "productId": "PROD-002",
        "quantity": 1,
        "unitPrice": 99.00
      }
    ]
  }'

curl -X GET http://localhost:8080/api/v1/orders/6a91fde3b601cedf199e22eb

curl -X GET http://localhost:8080/api/v1/orders

curl -X GET "http://localhost:8080/api/v1/orders?orderId=6a91fde3b601cedf199e22eb"

curl -X GET "http://localhost:8080/api/v1/orders?partnerId=6a91fdb4197c2faa77c2158e"

curl -X GET "http://localhost:8080/api/v1/orders?status=APPROVED"

curl -X GET "http://localhost:8080/api/v1/orders?startDate=2026-08-01T00:00:00Z&endDate=2026-08-31T23:59:59Z"

curl -X GET "http://localhost:8080/api/v1/orders?startDate=2026-08-28T00:00:00Z"

curl -X GET "http://localhost:8080/api/v1/orders?partnerId=6a91fdb4197c2faa77c2158e&status=APPROVED&startDate=2026-08-01T00:00:00Z&endDate=2026-08-31T23:59:59Z"

curl -X GET "http://localhost:8080/api/v1/orders?status=CANCELLED"

curl -X GET "http://localhost:8080/api/v1/orders?orderId=6a91fde3b601cedf199e22eb&partnerId=6a91fdb4197c2faa77c2158e&status=APPROVED&startDate=2026-08-01T00:00:00Z&endDate=2026-08-31T23:59:59Z"

curl -X PATCH http://localhost:8080/api/v1/orders/6a91fde3b601cedf199e22eb/cancel

curl -X PATCH http://localhost:8080/api/v1/orders/6a929a94aed32c5d26286a6e/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "DELIVERED"
  }'
  
Kafka UI
http://localhost:8085/

Mongo Express
http://localhost:8081/

Swagger
http://localhost:8080/swagger-ui.html
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```