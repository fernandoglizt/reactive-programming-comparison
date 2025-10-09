run:
	uvicorn app.main:app --host 0.0.0.0 --port $${PORT:-8084} --reload

build:
	docker build -t python-rxpy:local .

up:
	docker compose up -d python-rxpy

logs:
	docker logs -f python-rxpy

