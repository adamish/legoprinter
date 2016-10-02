/**
  * This file is part of lego110 firmware.
  *
  *  Lego110 is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 3 of the License, or
  *  (at your option) any later version.
  *
  *  Foobar is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with lego110 firmware.  If not, see <http://www.gnu.org/licenses/>.   
  */
#include <HardwareTimer2.h>

/** 
 * Manage an optocounter connected to a given analog pin. 
 */
class OptoCounter {
  private:
  // number of samples to average
  int sampleLength;
  // storage for sample averaging
  int * samples;
  // current digital state
  int state;
  // counter
  int count;
  // direction of head, -1 = left, 1 = right
  int dir; 
  
  public:
  // analog pin number to use.
  int pin;
  // ADC convertor (10bit) value to trigger on state  
  int thresholdOn;
  // ADC convertor (10bit) value to trigger off state
  int thresholdOff;
  
  void setDir(int dir) {
    this->dir = dir;
  }

  void setPin(int pin) {
    this->pin = pin;
  }
  
  void setSampleLength(int sampleLength) {
    this->sampleLength = sampleLength;
    this->samples = (int *) new int[sampleLength];
  }
  
  /**
   * Monitor for state changes in optosensor.
   * Average samples to reduce noise.
   */
  void sample() {
    int sample = analogRead(pin);
    
    for (int i = 0; i < sampleLength - 1; i++) {
      samples[i] = samples[i + 1];
    }    
    samples[sampleLength - 1] = sample;
    int sum = 0;
    for (int i = 0; i < sampleLength; i++) {
     sum = sum + samples[i];
    }
    int average = sum / sampleLength;
    
    boolean newstate = state;
    if (average > thresholdOn) {
      newstate = 1;
    } else if (average < thresholdOff) {
      newstate = 0;
    }
    if (state != newstate) {
      count = count + dir;
    }
    state = newstate;  
  }
  
  /**
   * Set counter (typicall after return to home position).
   */
  void setCount(int newcount) {
    count = newcount;
  }
  
  /**
   * Get current count position.
   * @return Position where 0 is hard left, and N is hard right.
   */
  int getCount() {
    return count;
  }
  
};



/** Simple software PWM. */
class Pwm {
private:
  // counter 0 < x < cycle
  int counter;
  // pulse period
  int period;
  // between 0 < x < cycle controls duty cycle.
  int power;

public:
  // current of wave
  int state;
  
  /**
   * Period of waveform.
   */
  void setPeriod(int period) {
    this->period = period;
  }
  
  /*
   * Duty cycle.
   * @param duty 0..1
   */
  void setDuty(float duty) {
    int power = (int)((float)period * duty);
    this->power = power;
  }

  /** 
    * PWM tick routine to form the up/down transitions.
    */
  void tick() {
    if (counter % period < power) {
      state = 1;
    } else {
      state = 0;
    }
    counter = (counter + 1) % period;
  }

  /**
   * Reset head movement PWM to default.
   */
  void reset() {
    counter = 0;
    state = 0;
  }
};


/**
 * High level control of the motors.
 */
class MotorControl {
  private:
    Pwm * pwm;
    
  public:
  int pinHeadHorizA;
  int pinHeadHorizB;
  int pinHeadVertA;
  int pinHeadVertB;
  int pinPaperA;
  int pinPaperB;
  int pinStopLeft;
  int pinStopRight;
  int penDownDelay;
  int penUpDelay;
  OptoCounter * headCounter;
  // power settings
  float powerMax;
  float powerSlow;
  float powerSlower;
  double headTimeout;
  double clockMillisecs;
  
  MotorControl() {
    pwm = new Pwm();
    pwm->setPeriod(300);
  }
  
  /**
   * Turn print head motor in left direction.
   */
  void headLeft() {
    digitalWrite(pinHeadHorizA, 1);
    digitalWrite(pinHeadHorizB, 0);
  }

  /**
   * Turn print head motor in right direction.
   */
  void headRight() {
    digitalWrite(pinHeadHorizA, 0);
    digitalWrite(pinHeadHorizB, 1);
  } 

  /**
   * Turn off head control motor.
   */
  void headStop() {
    digitalWrite(pinHeadHorizA, 1);
    digitalWrite(pinHeadHorizB, 1);
  }

  /**
   * Turn on paper feed motor so paper moves towards print head.
   */
   void paperDown() {
    digitalWrite(pinPaperA, 1);
    digitalWrite(pinPaperB, 0);
  }

  /**
   * Turn on paper feed motor so paper reverses out of printer.
   */
   void paperUp() {
    digitalWrite(pinPaperA, 0);
    digitalWrite(pinPaperB, 1);
  }

  /**
   * Turn off paper feed motor.
   */
   void paperStop() {
    digitalWrite(pinPaperA, 0);
    digitalWrite(pinPaperB, 0);
  }
  
  /**
   * Turn pen motor so pen moves towards paper.
   */
    void penDown() {
    digitalWrite(pinHeadVertA, 0);
    digitalWrite(pinHeadVertB, 1);
  }

  /**
   * Turn pen motor so pen moves away from paper.
   */
  void penUp() {
    digitalWrite(pinHeadVertA, 1);
    digitalWrite(pinHeadVertB, 0);
  }
  
  /**
   * Turn off pen motor (park).
   */
   void penStop() {
    digitalWrite(pinHeadVertA, 0);
    digitalWrite(pinHeadVertB, 0);
  }

  /**
   * Full pen lift.
   */
  void penUpFull() {
    penUp();
    delay(penUpDelay);
    penStop();
  }
  
  /**
   * Full pen drop.
   */
  void penDownFull() {
    penDown();
    delay(penDownDelay);
    penStop();
  }
  
  /**
   * Hard left sensor.
   * @return True if print head at sensor.
   */
   boolean isStopLeft() {
      int stopLeft = digitalRead(pinStopLeft);
      return stopLeft == 0;
  }

  /**
   * Hard right sensor.
   * @return True if print head at sensor.
   */
  boolean isStopRight() {
      int stopRight = digitalRead(pinStopRight);
      return stopRight == 0;
  }
  
  
  /**
   * Move left to a given position.
   * Typically blocking but with timeout mechanism if head jams.
   * @pos Absolute position managed by count mechanism.
   */
  void moveToLeft(int pos) {
    headCounter->setDir(-1);
    pwm->reset();
    pwm->setDuty(powerMax);
    this->clockMillisecs = 0;
    while (true) {
      if (pwm->state) {
        headLeft();
      } else {
        headStop();
      }
      int diff = headCounter->getCount() - pos;
      if (diff <= 0 || isStopLeft()) {
        break;
      }
      if (this->clockMillisecs > this->headTimeout) {
        Serial.print("T\n");
        break;
      }
      pwm->setDuty(getPowerForDiff(diff));
      pwm->tick();
    }
    headStop();
  }

  /**
   * Move right to a given position.
   * Typically blocking but with timeout mechanism if head jams.
   * @pos Absolute position managed by count mechanism.
   */
  void moveToRight(int pos) {
    headCounter->setDir(1);
    pwm->reset();
    pwm->setDuty(powerMax);
    this->clockMillisecs = 0;
    while (true) {
      if (pwm->state) {
        headRight();
      } else {
        headStop();
      }
      int diff = pos - headCounter->getCount(); 
      if (diff <= 0 || isStopRight()) {
        break;
      }
      if (this->clockMillisecs > this->headTimeout) {
        Serial.print("T\n");
        break;
      }
      pwm->setDuty(getPowerForDiff(diff));
      pwm->tick();
    }
    headStop();
  }
  
  /**
   * Noddy control mechanism to prevent overshoot of target.
   * @param Positive remaining distance to target by counter.
   * @return PWM power to use between 0 and powerMax.
   */
  float getPowerForDiff(int diff) {
    float returnValue = powerMax;
    if (diff < 15) {
      returnValue = powerSlow;
    } else if (diff < 8) {
      returnValue = powerSlower;
    }
    return returnValue;
  }
  
  /**
   * Goto absolute position.
   * @param pos Absolute position. 0 is hard left, 600 ish is hard right (relative to page).
   */
  void gotoPos(int pos) {
    if (pos > headCounter->getCount()) {
      moveToRight(pos);
    } else if (pos < headCounter->getCount()) {
      moveToLeft(pos);
    }
  } 

};

class LinePrinter {
  private:
  // length of line.
  int lineLength;
  // current line
  int * lineData;
  // direction, 1 = right, -1 = left.
  int dir; 

  public:
  MotorControl * motorControl;
  
  void setLineData(int * lineData) {
    this->lineData = lineData;
  }
  
  void setLineLength(int lineLength) {
    this->lineLength = lineLength;
  }
  
  void setDir(int dir) {
    this->dir = dir;
  }
  
  int getDir() {
    return this->dir;
  }
  
  /**
   * Print a line. 0 is blank, 1 is black (pen down).
   * @param pline Line of integers 0 or 1s. pline_length is array size.
   * @param length of line.
   */
  void print() {
  
    int i = 0;
    if (dir == 1) {
      i = 0;
    } else if (dir == -1) {
      i = lineLength - 1;
    }
    
    while (i >= 0 && i < lineLength && dir != 0) {
      // next print position
      int downPos = findNext(1, i);
      if (downPos == -1) {
        break;
      }
      motorControl->gotoPos(downPos);
      i = downPos;
      motorControl->penDownFull();
      
      // move to next up position
      int upPos = findNext(0, i);
      if (upPos == -1 && dir == 1) {
        upPos = lineLength - 1;
      } else if (upPos == -1 && dir == -1) {
        upPos = 0;
      }
      motorControl->gotoPos(upPos);
      i = upPos;
      motorControl->penUpFull();    
    }  
  }

  /**
   * Find next item in given direction.
   */
  int findNext(int element, int pos) {
    int returnValue = -1;
    int i = pos + dir;
    while (i >= 0 && i < lineLength && dir != 0) {
      if (lineData[i] == element) {
        returnValue = i;
        break;
      }
      i = i + dir;    
    }
    return returnValue;
  }
};


MotorControl * motorControl;
LinePrinter * linePrinter;

// buffer for incoming lines.
int lineMax = 1024;
int lineDataBuffer[1024];
double clockMillisecs = 0;

/**
 * Entry point, called externally.
 */
void setup() { 
  
  OptoCounter * headCounter = new OptoCounter();
  headCounter->pin = 0;
  headCounter->setSampleLength(2);
  headCounter->thresholdOn = 600;
  headCounter->thresholdOff = 400;
  pinMode(headCounter->pin, INPUT); 
      
  motorControl = new MotorControl();
  motorControl->headCounter = headCounter;
  motorControl->pinHeadHorizA = 16;
  motorControl->pinHeadHorizB = 17;
  motorControl->pinHeadVertA = 22;
  motorControl->pinHeadVertB = 23;
  motorControl->pinPaperA = 20;
  motorControl->pinPaperB = 21;
  motorControl->pinStopLeft = 6;
  motorControl->pinStopRight = 7;
  motorControl->penDownDelay = 430; // gravity on our side, takes less time.
  motorControl->penUpDelay = 800; // ensure return to start
  motorControl->powerMax = 1;
  motorControl->powerSlow = 0.83;
  motorControl->powerSlower = 0.6;
  motorControl->headTimeout=32000; // 32 seconds
  
  pinMode(motorControl->pinHeadHorizA, OUTPUT); 
  pinMode(motorControl->pinHeadHorizB, OUTPUT); 
  pinMode(motorControl->pinHeadVertA, OUTPUT); 
  pinMode(motorControl->pinHeadVertB, OUTPUT); 
  pinMode(motorControl->pinPaperA, OUTPUT); 
  pinMode(motorControl->pinPaperB, OUTPUT); 
  pinMode(motorControl->pinStopLeft, INPUT); 
  pinMode(motorControl->pinStopRight, INPUT);   
    
  linePrinter = new LinePrinter();
  linePrinter->motorControl = motorControl;
  linePrinter->setDir(1);
  
  Serial.begin(9600);
  delay(1000);
  Serial.print("Hello\n");
  
    // sample opto sensor every 1ms
  Timer2.set(1, timerTask);
  Timer2.start();
} 

void timerTask() {
  motorControl->headCounter->sample();
  motorControl->clockMillisecs++;
}

/**
 * Main loop, called externally.
 */
void loop() { 

  if( Serial.available() ) {       // if data is available to read 
    int value = Serial.read();

    // Reset head to 0 position, counters etc.
    if ('X' == value) {
      motorControl->headCounter->setCount(3000);
      motorControl->moveToLeft(0);
      motorControl->headCounter->setCount(0);
      serialAck();
    }
    
    // FXXX - form feed amount
    if ('F' == value) {
      int amount = getSerialAscDec(3);
      motorControl->paperDown();
      delay(amount);
      motorControl->paperStop();
      serialAck();
    }
    
    // PXXX print a number of points 1..N 0 or 1
    if ('P' == value) {
      boolean more = true;
      int lineLength = 0;
      while (more && lineLength < lineMax) {
        int value = getSerialChr();
        if (value == 10 || value == 13 || value ==  33) {
          more = false;
        } else {
          int newElement = value - 48;
          lineDataBuffer[lineLength] = newElement;
          lineLength++; 	
        }    
      }
      linePrinter->setLineData(lineDataBuffer);
      linePrinter->setLineLength(lineLength);
      linePrinter->print();
      if (linePrinter->getDir() == 1) {
        linePrinter->setDir(-1); // was right, now left.
      } else {
        linePrinter->setDir(1); // was left, now right.
      }
      serialAck();
    }
        
    // constant control
    if ('K' == value) {
      int val = getSerialChr();
      if ('1' == val) {
        motorControl->penDownDelay = getSerialAscDec(3);
      } else if ('2' == val) {
        motorControl->penUpDelay = getSerialAscDec(3);
      } else if ('3' == val) {    
        motorControl->powerMax = getSerialAscDec(3);    
      } else if ('4' == val) {
        motorControl->powerSlow = getSerialAscDec(3);
      } else if ('5' == val) {
        motorControl->powerSlower = getSerialAscDec(3);
      }
      serialAck();                                                        
    }      
    
    // hard right test
    if ('R' == value) {
      motorControl->headCounter->setCount(0);
      motorControl->moveToRight(3000);
      serialAck();           
    }
    
    // hard left test
    if ('L' == value) {
      motorControl->headCounter->setCount(3000);
      motorControl->moveToLeft(0);
      serialAck();
    }
    
    // GXXX = goto position test
    if ('G' == value) {
      int amount = getSerialAscDec(3);
      motorControl->gotoPos(amount);
      serialAck();
    }
    
    // Debug info
    if ('D' == value) {
      Serial.print("D=");
      Serial.print(motorControl->headCounter->getCount());
      Serial.print(",S=");
      if (motorControl->isStopLeft()) {
        Serial.print("L");
      }
      if (motorControl->isStopRight()) {
        Serial.print("R");
      }
      Serial.print("\n");
      serialAck();
    }
  }
} 


/**
 * Read next N ASCII decimal bytes from serial and return
 * decimal equivalent. Blocks till data available.
 * i.e. "123" -> 48 49 50 -> 123.
 * @param digits Number of digits to expect 1..N.
 * @return decimal value.
 */
int getSerialAscDec(int digits) {
  int returnValue = 0; 
  int c = 0;
  // 123
  // 1 * 10E2 + 2 * 10E1 + 3 * 10E0
  while (c < digits) {
    if( Serial.available()) {
      int digit = Serial.read() - 48;
      returnValue += digit * pow(10, digits - c - 1);
      c++;  
    }
  }
  return returnValue;
}

/**
 * Read next serial character, blocks till data available.
 * @return Next serial character.
 */
int getSerialChr() {
  int returnValue;
  while (true) {
    if( Serial.available()) {
      returnValue = Serial.read();
      break;
    }
  }
  return returnValue;
}

/**
 * Standard reply to serial commands.
 */
void serialAck() {
  Serial.print("OK\n");
}

