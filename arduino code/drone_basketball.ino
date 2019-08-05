#include <SoftwareSerial.h>//라이브러리를 불러옵니다
#include <Servo.h>

SoftwareSerial bt(3, 2);//블루투스 객체를 선언해줍니다

Servo servo;

void setup() {
  Serial.begin(9600);//시리얼 통신속도 설정
  bt.begin(9600);//블루투스 통신속도 설정
  servo.attach(5);//서보모터는 5번 pwm핀에 연결됨
  servo.write(55);//서보 위치를 55로 설정

  pinMode(4, OUTPUT);//4번핀을 출력핀으로 설정
}

void loop() {
  String Sinput = "";
  while (Serial.available()) {
    bt.write(Serial.read());
  }
  
  if (Sinput != "") {
    sendSignal(Sinput);
  }

  bool ballcheck = digitalRead(A0);
  int bomb = analogRead(A2);

  if (ballcheck == false) {
    if (bomb > 30) {
      sendSignal(String(bomb));
      delay(100);
      bomb = 0;
    }
  }

  String input = "";
  while (bt.available() > 0) {
    input += (char) bt.read();
    delay(1);
  }
  if (input == "grip") {
    servo.write(0);
  }
  if (input == "drop") {
    servo.write(55);
  }
  if (input == "mdrop") {
    servo.write(55);
    digitalWrite(4, HIGH);
    delay(1500);
    digitalWrite(4, LOW);
  }
  if (input == "buzz") {
    digitalWrite(4, HIGH);
    delay(200);
    digitalWrite(4, LOW);
    delay(100);
    digitalWrite(4, HIGH);
    delay(200);
    digitalWrite(4, LOW);
  }
}

void sendSignal(String input) {
  bt.write(('0' + input).c_str());
}

