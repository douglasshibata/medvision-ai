import requests
import os
import time

def test_medvision_flow():
    # 1. Simular Upload de DICOM
    print("Simulando upload de DICOM...")
    # Criamos um arquivo fake de DICOM (em um teste real, usaríamos um .dcm de verdade)
    with open("test.dcm", "wb") as f:
        f.write(b"DICM_FAKE_DATA")

    upload_url = "http://localhost:8080/api/v1/upload-dicom"
    # files = {'file': open('test.dcm', 'rb')}
    # try:
    #     response = requests.post(upload_url, files=files)
    #     print(f"Status do Upload: {response.status_code}")
    #     print(f"Resposta: {response.text}")
    # except Exception as e:
    #     print(f"Erro no upload (o servidor está rodando?): {e}")

    # 2. Verificar Fila de Triagem
    print("Verificando fila de triagem...")
    results_url = "http://localhost:8080/api/v1/results"
    # try:
        # response = requests.get(results_url)
        # print(f"Resultados na Triagem: {response.json()}")
    # except Exception as e:
        # print(f"Erro ao buscar resultados: {e}")

    print("Fluxo de verificação concluído (simulado).")

if __name__ == "__main__":
    test_medvision_flow()
