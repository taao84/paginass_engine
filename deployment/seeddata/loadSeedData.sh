#!/usr/bin/env bash

docker exec -i postgres psql -U paginass -d paginass < ./seed_data.sql