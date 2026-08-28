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

curl -X GET http://localhost:8080/api/v1/orders/6a913a1ae66f803fab58605a
```