#!/usr/bin/env python3
"""
Test de Carga para el Object Pool del Messaging Server.

Abre (maxConnections + 1) conexiones TCP concurrentes contra el servidor.
Las primeras maxConnections deben ser aceptadas; la última (extra) debe
recibir el JSON de rechazo:
    {"status": "error", "message": "Servidor sobrecargado. Intente más tarde."}

Uso:
    python pool_load_test.py [host] [port] [maxConnections]
    python pool_load_test.py localhost 8080 5

Requisitos: Python 3.6+
"""

import socket
import sys
import threading
import time

DEFAULT_HOST = "localhost"
DEFAULT_PORT = 8080
DEFAULT_MAX_CONNECTIONS = 5
TIMEOUT_SECONDS = 10


def create_persistent_connection(host, port, conn_id, results, barrier):
    """
    Crea una conexión TCP que se mantiene abierta (simula un cliente activo).
    Envía un mensaje simple y espera la respuesta.
    """
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(TIMEOUT_SECONDS)
        sock.connect((host, port))

        # Sincronizar: esperar a que todos los hilos estén conectados
        barrier.wait(timeout=TIMEOUT_SECONDS)

        # Enviar un mensaje de prueba
        message = f'{{"action": "ping", "clientId": {conn_id}}}'
        sock.sendall(message.encode("utf-8"))

        # Leer respuesta
        response = sock.recv(4096).decode("utf-8")
        results[conn_id] = {"status": "ACCEPTED", "response": response}
        print(f"  [Conn {conn_id:>3}] ✅ ACEPTADA — Respuesta: {response}")

        # Mantener la conexión abierta para saturar el pool
        time.sleep(3)

    except socket.timeout:
        results[conn_id] = {"status": "TIMEOUT", "response": None}
        print(f"  [Conn {conn_id:>3}] ⏱️  TIMEOUT — Sin respuesta en {TIMEOUT_SECONDS}s")
    except ConnectionRefusedError:
        results[conn_id] = {"status": "REFUSED", "response": None}
        print(f"  [Conn {conn_id:>3}] ❌ RECHAZADA — Conexión rehusada por el servidor")
    except Exception as e:
        results[conn_id] = {"status": "ERROR", "response": str(e)}
        print(f"  [Conn {conn_id:>3}] ❌ ERROR — {e}")
    finally:
        try:
            sock.close()
        except Exception:
            pass


def create_overflow_connection(host, port, conn_id, results):
    """
    Crea la conexión extra (#maxConnections + 1) que debe ser RECHAZADA por el pool.
    Espera recibir el JSON de rechazo.
    """
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(TIMEOUT_SECONDS)
        sock.connect((host, port))

        # Leer la respuesta de rechazo (el servidor la envía inmediatamente)
        response = sock.recv(4096).decode("utf-8")

        if "error" in response.lower() or "sobrecargado" in response.lower():
            results[conn_id] = {"status": "REJECTED_OK", "response": response}
            print(f"  [Conn {conn_id:>3}] 🚫 RECHAZADA (ESPERADO) — {response}")
        else:
            results[conn_id] = {"status": "UNEXPECTED_ACCEPT", "response": response}
            print(f"  [Conn {conn_id:>3}] ⚠️  INESPERADO: Se esperaba rechazo — {response}")

    except socket.timeout:
        results[conn_id] = {"status": "TIMEOUT", "response": None}
        print(f"  [Conn {conn_id:>3}] ⏱️  TIMEOUT — Sin respuesta (posible pool no saturado)")
    except ConnectionRefusedError:
        results[conn_id] = {"status": "REFUSED", "response": None}
        print(f"  [Conn {conn_id:>3}] ❌ RECHAZADA — Conexión rehusada (servidor caído?)")
    except Exception as e:
        results[conn_id] = {"status": "ERROR", "response": str(e)}
        print(f"  [Conn {conn_id:>3}] ❌ ERROR — {e}")
    finally:
        try:
            sock.close()
        except Exception:
            pass


def main():
    host = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_HOST
    port = int(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_PORT
    max_conn = int(sys.argv[3]) if len(sys.argv) > 3 else DEFAULT_MAX_CONNECTIONS

    total = max_conn + 1  # +1 para provocar el rechazo

    print("=" * 60)
    print("  TEST DE CARGA — Object Pool (Messaging Server)")
    print("=" * 60)
    print(f"  Host:              {host}")
    print(f"  Puerto:            {port}")
    print(f"  Max Connections:   {max_conn}")
    print(f"  Total a lanzar:    {total} (max + 1 para overflow)")
    print("=" * 60)
    print()

    results = {}
    threads = []

    # Barrera para sincronizar que todos los hilos se conecten "a la vez"
    barrier = threading.Barrier(max_conn)

    # Lanzar las primeras maxConnections conexiones (deben ser aceptadas)
    print(f"[1/2] Lanzando {max_conn} conexiones concurrentes (deben ser ACEPTADAS)...\n")
    for i in range(max_conn):
        t = threading.Thread(target=create_persistent_connection, args=(host, port, i, results, barrier))
        threads.append(t)
        t.start()

    # Esperar un momento para que el pool se sature
    time.sleep(1)

    # Lanzar la conexión extra (debe ser RECHAZADA)
    print(f"\n[2/2] Lanzando conexión extra #{total} (debe ser RECHAZADA)...\n")
    overflow_thread = threading.Thread(target=create_overflow_connection, args=(host, port, max_conn, results))
    overflow_thread.start()
    threads.append(overflow_thread)

    # Esperar a que todos finalicen
    for t in threads:
        t.join(timeout=TIMEOUT_SECONDS + 5)

    # Resumen
    print("\n" + "=" * 60)
    print("  RESUMEN DE RESULTADOS")
    print("=" * 60)

    accepted = sum(1 for r in results.values() if r["status"] == "ACCEPTED")
    rejected = sum(1 for r in results.values() if r["status"] == "REJECTED_OK")
    errors = sum(1 for r in results.values() if r["status"] in ("ERROR", "TIMEOUT", "REFUSED"))
    unexpected = sum(1 for r in results.values() if r["status"] == "UNEXPECTED_ACCEPT")

    print(f"  ✅ Aceptadas:              {accepted}/{max_conn}")
    print(f"  🚫 Rechazadas (esperado):  {rejected}/1")
    print(f"  ⚠️  Aceptadas inesperadas: {unexpected}")
    print(f"  ❌ Errores/Timeouts:       {errors}")
    print()

    if accepted == max_conn and rejected == 1 and unexpected == 0:
        print("  🎉 TEST EXITOSO: El Object Pool funciona correctamente.")
        print("     El pool aceptó exactamente maxConnections y rechazó la extra.")
    else:
        print("  ⚠️  TEST CON OBSERVACIONES: Revise los resultados detallados arriba.")

    print("=" * 60)


if __name__ == "__main__":
    main()
