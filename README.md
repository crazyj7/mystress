# System Stress Test Simulator

Java 8 이상에서 작동하는 TCP/UDP 스트레스 테스트 시뮬레이션 프로그램입니다.

## 기능

- **네트워크 스트레스 테스트**
  - TCP 및 UDP 서버/클라이언트 기능 포함
  - 양방향 통신 지원 (서버와 클라이언트가 서로 통신)
  - 대량의 동시 연결 생성
  - 포트 범위 지정 가능
  - 연결 실패 시 자동 재시도
- **파일 시스템 스트레스 테스트**
  - 파일 생성, 읽기, 쓰기, 추가, 이름 변경, 삭제
  - 디렉토리 생성 및 삭제
  - 랜덤 파일 크기 및 서브 디렉토리 구조 생성
  - 테스트 종료 시 자동 정리
- **설정 및 배포**
  - JSON 설정 파일로 모든 파라미터 관리
  - Runnable JAR 형태로 배포 가능
  - 각 테스트 모듈별 on/off 제어

## 빌드 방법

```bash
mvn clean package
```

빌드 후 `target/mystress-1.0.0.jar` 파일이 생성됩니다.

## 실행 방법

```bash
# 기본 설정 파일 사용
java -jar target/mystress-1.0.0.jar

# 설정 파일 지정
java -jar target/mystress-1.0.0.jar config.json

# Peer IP 지정 (설정 파일의 serverHost를 오버라이드)
java -jar target/mystress-1.0.0.jar 192.168.1.100

# 설정 파일과 Peer IP 모두 지정
java -jar target/mystress-1.0.0.jar config.json 192.168.1.100
```

설정 파일 경로를 지정하지 않으면 기본적으로 `config.json`을 사용합니다.
Peer IP를 지정하면 설정 파일의 `serverHost` 값을 오버라이드합니다.

## 설정 파일 (config.json)

```json
{
  "server": {
    "tcpEnabled": true,
    "tcpPortRange": {
      "start": 8000,
      "end": 8100
    },
    "udpEnabled": true,
    "udpPortRange": {
      "start": 9000,
      "end": 9100
    }
  },
  "client": {
    "tcpEnabled": true,
    "tcpThreadCount": 100,
    "tcpPortRange": {
      "start": 8000,
      "end": 8100
    },
    "udpEnabled": true,
    "udpThreadCount": 50,
    "udpPortRange": {
      "start": 9000,
      "end": 9100
    },
    "serverHost": "localhost"
  },
  "networkTest": {
    "enabled": true,
    "dataSize": 1024,
    "iterations": 1000,
    "delayBetweenIterations": 100,
    "delayBetweenConnections": 100
  },
  "fileTest": {
    "enabled": false,
    "testFolderPath": "./test_stress",
    "threadCount": 10,
    "iterations": 100,
    "minFileSize": 1024,
    "maxFileSize": 1048576,
    "maxSubDirDepth": 3,
    "delayBetweenOperations": 10,
    "deleteProbability": 0.1,
    "mkdirProbability": 0.1,
    "rmdirProbability": 0.05
  }
}
```

### 설정 항목 설명

#### server
- `tcpEnabled`: TCP 서버 활성화 여부
- `tcpPortRange`: TCP 서버가 리스닝할 포트 범위 (start ~ end)
- `udpEnabled`: UDP 서버 활성화 여부
- `udpPortRange`: UDP 서버가 리스닝할 포트 범위 (start ~ end)

#### client
- `tcpEnabled`: TCP 클라이언트 활성화 여부
- `tcpThreadCount`: 동시 실행할 TCP 클라이언트 스레드 개수
- `tcpPortRange`: TCP 클라이언트가 연결할 포트 범위
- `udpEnabled`: UDP 클라이언트 활성화 여부
- `udpThreadCount`: 동시 실행할 UDP 클라이언트 스레드 개수
- `udpPortRange`: UDP 클라이언트가 전송할 포트 범위
- `serverHost`: 서버 호스트 주소 (예: "localhost", "192.168.1.100")

#### networkTest
- `enabled`: 네트워크 테스트 활성화 여부 (false로 설정하면 TCP/UDP 서버 및 클라이언트 모두 비활성화)
- `dataSize`: 송수신할 데이터 크기 (바이트)
- `iterations`: 각 스레드당 반복 횟수
- `delayBetweenIterations`: 반복 사이의 지연 시간 (밀리초)
- `delayBetweenConnections`: 연결 사이의 지연 시간 (밀리초)

#### fileTest
- `enabled`: 파일 스트레스 테스트 활성화 여부
- `testFolderPath`: 테스트를 수행할 폴더 경로
- `threadCount`: 동시 실행할 파일 테스트 스레드 개수
- `iterations`: 각 스레드당 반복 횟수
- `minFileSize`: 생성할 파일의 최소 크기 (바이트)
- `maxFileSize`: 생성할 파일의 최대 크기 (바이트)
- `maxSubDirDepth`: 최대 서브 디렉토리 깊이
- `delayBetweenOperations`: 작업 사이의 지연 시간 (밀리초)
- `deleteProbability`: 파일 삭제 확률 (0.0 ~ 1.0)
- `mkdirProbability`: 디렉토리 생성 확률 (0.0 ~ 1.0)
- `rmdirProbability`: 디렉토리 삭제 확률 (0.0 ~ 1.0)

## 동작 방식

### 네트워크 스트레스 테스트

1. `networkTest.enabled`가 `true`인 경우에만 실행됩니다.
2. 서버가 지정된 포트 범위에서 TCP/UDP 리스너를 시작합니다.
3. 클라이언트 스레드들이 랜덤한 포트로 연결을 시도하고 데이터를 송수신합니다.
4. **연결 실패 시 1초마다 자동으로 재시도합니다.**
5. 각 연결/전송 후 즉시 close()하여 새로운 연결을 생성합니다.
6. 이 과정을 반복하여 시스템에 스트레스를 가합니다.

### 파일 스트레스 테스트

1. `fileTest.enabled`가 `true`인 경우에만 실행됩니다.
2. 지정된 테스트 폴더 내에서 파일 및 디렉토리 작업을 수행합니다.
3. 각 스레드는 설정된 확률에 따라 다양한 작업을 수행합니다:
   - 파일 생성, 읽기, 쓰기, 추가, 이름 변경, 삭제
   - 디렉토리 생성 및 삭제
4. 모든 작업은 디버그 콘솔 로그로 출력됩니다.
5. 프로그램 종료 시 테스트 폴더 내부가 자동으로 정리됩니다.

## 종료 방법

Ctrl+C를 누르면 graceful shutdown이 수행되며 통계 정보가 출력됩니다.


## 통계 정보

프로그램 종료 시 다음 통계가 출력됩니다:

### 네트워크 테스트 통계
- TCP/UDP 서버가 처리한 연결/패킷 수
- 서버가 수신한 총 바이트 수
- 클라이언트가 생성한 연결/패킷 수
- 클라이언트가 전송한 총 바이트 수

### 파일 테스트 통계
- 파일 생성 횟수
- 파일 읽기 횟수
- 파일 쓰기 횟수
- 파일 추가(append) 횟수
- 파일 이름 변경 횟수
- 파일 삭제 횟수
- 디렉토리 생성/삭제 횟수
- 총 읽기/쓰기 바이트 수

## 주의사항

- 파일 스트레스 테스트는 지정된 테스트 폴더 내에서만 작업을 수행합니다.
- 테스트 폴더는 프로그램 종료 시 자동으로 정리되지만, 강제 종료 시에는 수동으로 정리해야 할 수 있습니다.
- 네트워크 테스트는 대량의 연결을 생성하므로 방화벽 설정을 확인하세요.
- 포트 범위는 시스템의 사용 가능한 포트 수를 고려하여 설정하세요.

