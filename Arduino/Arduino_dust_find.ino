#include <Wire.h>  
#include <LiquidCrystal_I2C.h>   //LCD 라이브러리
#include <SoftwareSerial.h>  // 블루투스 라이브러리
#include <Adafruit_NeoPixel.h> // 네오픽셀링 라이브러리
#include "sound_effect.h"  // 사운드 이펙트 사용(음원효과)
#include <DHT11.h>    //온습도 라이브러리
#include <Timer.h> // 타이머 라이브러리

#define PIN 5
//네오픽셀을 사용하기 위해 객체 하나를 생성한다. 
//첫번째 인자값은 네오픽셀의 LED의 개수
//두번째 인자값은 네오픽셀이 연결된 아두이노의 핀번호
//세번째 인자값은 네오픽셀의 타입에 따라 바뀌는 flag
Adafruit_NeoPixel strip = Adafruit_NeoPixel(20, PIN, NEO_GRBW + NEO_KHZ800);
LiquidCrystal_I2C lcd(0x3F,16,2);  //LCD 주소 할당 
DHT11 dht11(10); // 온습도 센서 핀 연결
Timer Ts; // 타이머 변수

int TxPin = 2; //블루투스 
int RxPin = 3;
int pin = 8;  //먼지센서
int buzzer_pin = 13; //부저
unsigned long duration;   //지속 시간
unsigned long sampleDusttime_ms = 10000;   //먼지 샘플시간 10초 마다 업데이트
unsigned long sampleTemtime_ms = 20000;   //온도 샘플시간 20초 마다 업데이트
unsigned long lowpulseoccupancy = 0;   //Low 신호가 지속된 시간을 초기화
float ratio = 0;  //비율
float concentration = 0;  //입자 농도 0으로 초기화
float pcsPerCF = 0;  //한 입자당 CF를 0으로 초기화
float ugm3 = 0;  //최종 값으로 세제곱미터 당 마이크로 그램(㎍/㎥)
float ugm3Avg = 0; // 먼지센서값 평균값을 구하기 위한 임시 저장 공간
uint8_t clock[8] = {0x0, 0xe, 0x15, 0x17, 0x11, 0xe, 0x0};  // 시계 이모티콘
boolean ledOnOff = false; // LED on/off
boolean soundOnOff = false; // 사운드 on/off
SoftwareSerial BTSerial(TxPin, RxPin); 
double dust[] = {10.18, 54.22, 136.28, 221.99}; //테스트 코드
int index = 0;

void setup() {
  Serial.begin(9600);  //시리얼 통신 시작
  BTSerial.begin(9600);
  strip.begin(); //네오픽셀을 초기화하기 위해 모든LED를 off시킨다
  strip.show(); 
  pinMode(6, OUTPUT);       // Motor A 방향설정1
  pinMode(7, OUTPUT);       // Motor A 방향설정2
  pinMode(8,INPUT);  //미세먼지 센서 입력
  pinMode(13,OUTPUT);   //부저 출력
  lcd.begin();  //LCD 시작
  lcd.backlight();  //백라이트 ON
  lcd.clear();  //LCD 초기화
  lcd.print("Hello! SANGJI!");    //다음 문자를 LDC에 출력
  lcd.setCursor(0, 1);  //두 번째 줄로 커서 이동
  lcd.print("I will find DUST");
  lcd.createChar(1, clock);  //시계 이모티콘 출력
  seSqueak(13);  //초기시작 음원효과
  delay(5000);  // 5초 대기
  if ( ugm3 == 0) {    //만약 결과값이 0보다 작으면 아래를 LCD에 출력한다.
    lcd.clear();
    lcd.print("Analysing Data");
    lcd.write(1);
    lcd.setCursor(0, 1);
    lcd.print("................");
    lowpulseoccupancy = 0;
  }
  /*
   *every 함수를 통해 몇초동안 어떤 함수를 실행시킬 것인지 설정
   *첫번째 매개변수는 업데이트 할 시간 두번째 매개변수는 실행시킬 함수명    
  */
  Ts.every(sampleDusttime_ms, doDust); 
  Ts.every(sampleTemtime_ms, doTemperatur); 
}

void doDust() {
  
  digitalWrite(6, HIGH);     // Motor A 방향설정1
  digitalWrite(7, LOW);      // Motor A 방향설정2
  for (int i = 0; i < 10; i++) {
    duration = pulseIn(pin, LOW); //먼지센서의 LOW 지속시간 읽어오기 
    lowpulseoccupancy = lowpulseoccupancy+duration;
    ratio = lowpulseoccupancy/(sampleDusttime_ms*10.0);  // 정수 백분율
    concentration = 1.1*pow(ratio,3)-3.8*pow(ratio,2)+520*ratio+0.62; // 미세먼지 센서 사양 시트 곡선 사용
    pcsPerCF = concentration * 100;  // 입자 농도에 100을 곱하면 입자당 CF값
    ugm3 = pcsPerCF / 13000;  //입자당 CF를 13000으로 나누면 미터세제곱당 마이크로그람의 미세먼지 측정값
    ugm3Avg += ugm3;
   }
   ugm3 = ugm3Avg / 10;

//   ugm3 = dust[index]; // 테스트 코드
//   index++;
//   if (index == 4) {
//    index = 0;
//   }
   
  if (ugm3 > 0.01 ) {   // 만약에 결과값이 0.01보다 크면 미세먼지 값을 출력하라 
      String dust = String(ugm3);
      dust.concat("#");
      BTSerial.println(dust);
      lcd.clear();
      lcd.print("Dust:");
      lcd.print(ugm3);
      lcd.print("ug/m3");
      lowpulseoccupancy = 0;
      ugm3Avg = 0;
    }
  
    if (ugm3 > 0.01 && ugm3 <= 30) {   //만약 미세먼지 값이 0.01 보다 크고 30이랑 같거나 작으면 아래를 출력
      lcd.setCursor(0, 1);
      lcd.print("Good! ^v^");
      setColor(0, 0, 225, ledOnOff);  //블루
      analogWrite(9, 100);
      noTone(13);    //소리 끔
    }
    else if (ugm3 > 30 && ugm3 <= 80) {  //만약 미세먼지 값이 30보다 크고 80이랑 같거나 작으면 아래를 출력
      lcd.setCursor(0, 1);
      lcd.print("SoSo! ^ ^;");
      setColor(0, 255, 225, ledOnOff);  //블루그린    
      analogWrite(9, 150);
      noTone(13);   //소리 끔
    }
    else if (ugm3 > 80 && ugm3 <= 150) {  //만약 미세먼지 값이 80보다 크고 150이랑 같거나 작으면 아래를 출력
      lcd.setCursor(0, 1);
      lcd.print("Bad! T.T");
      setColor(255, 120, 0, ledOnOff);  //오렌지
      analogWrite(9, 200);
      noTone(13);    //소리 끔
    }
    else if (ugm3 > 150) {  //만약 미세먼지 값이 150 보다 크면 아래를 출력
      lcd.setCursor(0, 1);
      lcd.print("Be Careful @.@");
      setColor(255, 0, 0, ledOnOff);  //레드
      analogWrite(9, 250);
      setSound(soundOnOff);
    }
}

void doTemperatur() {

      float humi, temp;
      dht11.read(humi, temp);
      
      String tempstr = String(temp);
      tempstr.concat("!");
      BTSerial.println(tempstr);

}


void loop() {

  Ts.update(); // dustTs변수에 지정했던 every함수에서 설정했던 값을 실행
  Ts.update();
  
  if (BTSerial.available()) {
    char data = (char)BTSerial.read();
    if (data == '0') { // 안드로이드에서 블루투스 연결 성공할때 아두이노로 0이라는 값을 넘겨줌
      bluetoothConnectView(); // 안드로이드와 아두이노가 연결 성공했을 때 먼지농도, 온도 값을 넘겨줌
    } else if (data == '1') { //안드로이드-> 아두이노 LED ON
        ledOnOff = true;
        if (ugm3 > 0.01 && ugm3 <= 30) setColor(0, 0, 225, ledOnOff);
        else if (ugm3 > 30 && ugm3 <= 80) setColor(0, 255, 225, ledOnOff); 
        else if (ugm3 > 80 && ugm3 <= 150) setColor(255, 120, 0, ledOnOff);
        else if (ugm3 > 150) setColor(255, 0, 0, ledOnOff);
    } else if (data == '2') { //LED OFF
        setColor(0, 0, 0, true);  // 색 끄기
        ledOnOff = false;
    }  else if (data == '3') { //사운드 ON
        soundOnOff = true;
    } else if (data == '4') { // 사운드 OFF
        soundOnOff = false;
        setSound(false);
    }
  }
}

void bluetoothConnectView() {

  String dust = String(ugm3);
  dust.concat("#");
  BTSerial.println(dust);

  float humi, temp;
  dht11.read(humi, temp);

  String tempstr = String(temp);
  tempstr.concat("!");
  BTSerial.println(tempstr);
}

void setColor(int R, int G, int B, boolean ledOnOff) {
  if (ledOnOff) { 
    for(uint16_t i=0; i<strip.numPixels(); i++) {
      strip.setPixelColor(i, strip.Color(R,G,B) );
    }
    strip.show();  
  }
}

void setSound(boolean soundOnOff) {
  if (soundOnOff) {
    tone(13, 500);    //500Hz로 소리 재생
  } else {
    noTone(13);
  }
}


