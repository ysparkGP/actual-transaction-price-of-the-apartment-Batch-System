## 아파트 실거래가 알림 API

---
### 배치 흐름
![uml 다이어그램](https://user-images.githubusercontent.com/64354998/171605625-2a0cc778-c336-49e0-b575-3c84aefec762.PNG)
##### 데이터 출처) 국토 교통부 아파트 실거래가 Open API

---
### 테이블 설계
![엔티티 관계](https://user-images.githubusercontent.com/64354998/171606527-e43c06b2-8c15-4294-bdc8-591e143a6e0c.PNG)

---
### 주요 코드와 흐름
* ##### 동 코드 저장 배치 작업
![lawdCdJob](https://user-images.githubusercontent.com/64354998/171607232-eee8d5f1-e34e-479a-a3c6-fdef14001003.PNG)
![lawdCdJob2](https://user-images.githubusercontent.com/64354998/171607238-d40965cc-2094-486d-bf42-5f35d67cc4a2.PNG)
  * 다운로드 받은 동코드를 jobParameter로 넘겨주어 job을 실행한다.
  * job은 하나의 step을 가지고 있으며, step은 각 하나의 reader와 writer를 가지고있다.
  * reader로는 FlatFileItemReader를 사용하며 FieldSetMapper를 재정의한 mapper를 사용하여 lawd 테이블과 매핑을 시켜준다.
  * reader에서 읽어들인 lawd 객체들을 writer에 넘겨주어 db에 영속화시킨다.
  * 동 코드 출처) https://www.code.go.kr/

* ##### 아파트 정보와 아파트 실거래가 저장 배치 작업
![aptdealinsertjob3](https://user-images.githubusercontent.com/64354998/171610253-7baa42e1-22a1-486d-b1e7-65f25871710a.PNG)
![aptdealinsertjob4](https://user-images.githubusercontent.com/64354998/171610568-47c8eafb-7aa5-415b-872d-dba27d75130d.PNG)
![aptdealinsertjob](https://user-images.githubusercontent.com/64354998/171610277-3e085c3d-d97a-4384-9060-3df0d727b564.PNG)
![aptdealinsertjob2](https://user-images.githubusercontent.com/64354998/171610281-809bd983-7218-450c-888d-839792413906.PNG)
  * jobParameter로는 yyyy-mm 형식의 문자열인 yearMonth와 구코드를 입력받는 guLawdCd를 입력받는다.
  * job은 두 개의 step을 가지고 있다. 두 개의 step을 사용하는 이유는 다음과 같다.
  * 첫 번째 스텝으로는 구 코드를 읽고 두 번째 스텝으로는 첫 스텝에서 읽어들인 구 코드의 입력받은 yearMonth에 대한 API를 호출하여 새롭게 생성된 실거래가 정보만 db에 영속화시킨다.
  * 그러므로 첫 스텝에서 쓰인 몇 번째 구 코드까지 읽어들였느냐를 저장하기 위해서 executionContext를 사용하였다.

* ##### 유저 알림 배치 작업
![aptnotificationjob](https://user-images.githubusercontent.com/64354998/171612846-4c063702-de35-49e3-932e-6756a8dc1f0e.PNG)
![aptnotificationjob2](https://user-images.githubusercontent.com/64354998/171612854-3f6e4639-a642-4509-9c06-d24470bce9f0.PNG)
![aptnotificationjob3](https://user-images.githubusercontent.com/64354998/171612863-da7ea247-8c8a-4249-b514-d9178dafdafc.PNG)
  * jobParameter로는 yyyy-mm-dd 형식의 문자열인 dealDate를 입력받는다.
  * 유저가 지정한 구 코드의 그리고 거래가 일어난 아파트 실거래 정보들을 이메일로 전송한다.

---
### 결과물
* ##### 동 코드 배치 작업
![select lawdcd2](https://user-images.githubusercontent.com/64354998/171613372-31ce67d5-0b74-4330-bf26-4e734db03917.PNG)
![select lawdcd](https://user-images.githubusercontent.com/64354998/171613369-7bf80f3f-0d84-42cf-9074-755b13ea3340.PNG)

* ##### 아파트 정보와 아파트 실거래가 배치 작업
![select aptdeal](https://user-images.githubusercontent.com/64354998/171613887-874fc604-ff8d-4fef-9832-6145be550eef.PNG)
![select aptdeal2](https://user-images.githubusercontent.com/64354998/171613892-2152b934-7818-4c8f-85cc-502d793e6706.PNG)
![select aptdeal3](https://user-images.githubusercontent.com/64354998/171613895-8f58031b-a9ed-4147-9332-43d946f79485.PNG)

* ##### 유저 알림 배치 작업
![select aptdealnotification](https://user-images.githubusercontent.com/64354998/171614052-04ce0c58-d365-4d74-8c6e-d6ca9fc6530b.PNG)
![select aptdealnotification2](https://user-images.githubusercontent.com/64354998/171614054-5faa8a0b-135a-44ed-8963-4aeee7dddf4f.PNG)

