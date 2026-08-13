# EC2 CI/CD 및 블루·그린 배포

## 흐름

```text
main push
  -> GitHub Actions 테스트
  -> Backend/Frontend Docker 이미지 빌드
  -> GHCR에 Git SHA 태그로 발행
  -> EC2의 비활성 색상(blue 또는 green) 기동
  -> Backend/Frontend health check
  -> Nginx 포트 전환
  -> 이전 색상 중지
```

DB와 Redis는 색상 전환 대상이 아니며 두 애플리케이션이 공용으로 사용한다. 배포 중에만 두
버전이 동시에 실행되고, 전환이 끝나면 이전 버전을 중지해 `t3.small`의 메모리를 확보한다.

## EC2 최초 설정

Ubuntu 24.04 기준으로 저장소의 설정 스크립트를 서버에 복사하여 한 번 실행한다.

```bash
chmod +x scripts/setup-ec2.sh
./scripts/setup-ec2.sh
exit
```

다시 접속한 뒤 다음 명령이 권한 오류 없이 동작해야 한다.

```bash
docker version
docker compose version
sudo nginx -t
```

보안 그룹 인바운드는 `80/tcp`를 서비스 사용자에게, `22/tcp`를 관리자 IP `/32`에만 허용한다.
HTTPS를 적용할 때 `443/tcp`를 추가한다. PostgreSQL, Redis, 애플리케이션 내부 포트는 외부에
공개하지 않는다.

## GitHub 설정

Repository settings의 Actions variables에 다음 값을 등록한다.

- `EC2_HOST`: EC2 Elastic IP
- `EC2_USER`: Ubuntu AMI의 경우 `ubuntu`
- `EC2_READY`: 최초 서버 설정과 SSH 확인이 끝난 뒤 `true`

Actions secrets에는 다음 값을 등록한다.

- `EC2_SSH_PRIVATE_KEY`: EC2 배포 전용 개인키 전체 내용
- `EC2_KNOWN_HOSTS`: 신뢰할 수 있는 경로에서 확인한 SSH host key

AWS 액세스 키는 GitHub에 넣지 않는다. 이 구성은 EC2 SSH와 GHCR만 사용하므로 장기 AWS 키가
필요 없다.

## 블루·그린 포트

| 색상 | Frontend | Backend |
| --- | ---: | ---: |
| blue | 3001 | 8081 |
| green | 3002 | 8082 |

모든 포트는 EC2의 `127.0.0.1`에만 바인딩한다. 외부 요청은 호스트 Nginx의 80번 포트로만
들어오며, Nginx 설정 검증과 health check가 모두 성공한 후에 활성 색상이 바뀐다.

## 수동 배포와 롤백

GitHub Actions의 `Deploy to EC2` 워크플로는 수동 실행도 지원한다. 실패한 신규 색상은 자동으로
중지되고 기존 색상은 유지된다. 직전 버전으로 되돌려야 하면 해당 커밋에서 워크플로를 다시
실행하거나 이전 이미지 SHA를 사용해 EC2에서 배포 스크립트를 실행한다.
