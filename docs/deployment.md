# EC2 CI/CD 및 블루·그린 배포

## 흐름

```text
main push
  -> GitHub Actions 테스트
  -> Backend/Frontend Docker 이미지 빌드
  -> GHCR에 Git SHA 태그로 발행
  -> EC2 self-hosted 배포 러너가 비활성 색상(blue 또는 green) 기동
  -> Backend/Frontend health check
  -> Nginx 포트 전환
  -> 이전 색상 중지
```

DB와 Redis는 색상 전환 대상이 아니며 두 애플리케이션이 공용으로 사용한다. 배포 중에만 두
버전이 동시에 실행되고, 전환이 끝나면 이전 버전을 중지한다. 현재 운영 인스턴스는
`c7i.xlarge`(4 vCPU, 8 GiB)다.

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

보안 그룹 인바운드는 `80/tcp`, `443/tcp`를 서비스 사용자에게, `22/tcp`를 관리자 IP `/32`에만
허용한다. PostgreSQL, Redis, 애플리케이션 내부 포트는 외부에
공개하지 않는다.

현재 HTTPS는 `/etc/nginx/ssl/osori-selfsigned.crt`와
`/etc/nginx/ssl/osori-selfsigned.key`를 사용한다. 두 경로는 배포 시 다시 생성되는 Nginx
템플릿에도 포함되어 있으므로 Blue/Green 전환 뒤에도 443 리스너가 유지된다. 자체 서명 인증서이므로
공개 운영 전에는 도메인용 공인 인증서로 교체해야 한다.

## GitHub 설정

EC2에 저장소 전용 GitHub Actions self-hosted runner를 `production` 라벨로 등록한다. Repository
settings의 Actions variable `EC2_READY`는 최초 서버 설정과 러너 확인이 끝난 뒤 `true`로 둔다.

배포 러너가 EC2 안에서 실행되므로 GitHub-hosted runner를 위해 SSH를 외부에 개방하거나 개인키를
GitHub secrets에 저장하지 않는다. AWS 액세스 키도 필요 없다. 22번 포트는 관리자 IP `/32`에만
허용한다.

## 블루·그린 포트

| 색상 | Frontend | Backend |
| --- | ---: | ---: |
| blue | 3001 | 8081 |
| green | 3002 | 8082 |

모든 포트는 EC2의 `127.0.0.1`에만 바인딩한다. 외부 요청은 호스트 Nginx의 80번 포트로만
들어오며, Nginx 설정 검증과 health check가 모두 성공한 후에 활성 색상이 바뀐다.

## 공개 배포 현황

호스트 Nginx가 `/deploy-status`를 직접 제공한다. 페이지는 2초마다 상태 JSON을 갱신하며 테스트,
이미지 빌드, 신규 색상 기동, health check, Nginx 전환, 완료 또는 실패 상태와 경과 시간을 표시한다.
GitHub Actions 실행자와 직전 성공 배포 이후 포함된 커밋 목록도 함께 표시한다.
애플리케이션 컨테이너와 분리되어 있어 Blue/Green 전환 중에도 확인할 수 있다.

```text
http://43.202.205.211/deploy-status
https://43.202.205.211/deploy-status
```

## 수동 배포와 롤백

GitHub Actions의 `Deploy to EC2` 워크플로는 수동 실행도 지원한다. 실패한 신규 색상은 자동으로
중지되고 기존 색상은 유지된다. 직전 버전으로 되돌려야 하면 해당 커밋에서 워크플로를 다시
실행하거나 이전 이미지 SHA를 사용해 EC2에서 배포 스크립트를 실행한다.
