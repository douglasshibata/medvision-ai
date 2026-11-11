FROM nvidia/cuda:13.2.1-runtime-ubuntu22.04
WORKDIR /app

RUN apt-get update && apt-get install -y python3 python3-pip && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt

COPY worker.py .
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

CMD ["python3", "worker.py"]
