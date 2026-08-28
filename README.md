```
docker compose up -d
docker-compose down -v

curl -X POST http://localhost:8080/api/v1/orders   -H "Content-Type: application/json"   -d '{
    "partnerId": "6a91383fdd80259c051fba78",
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

```