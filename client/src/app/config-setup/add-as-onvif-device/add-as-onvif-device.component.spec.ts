import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddAsOnvifDeviceComponent } from './add-as-onvif-device.component';

describe('AddAsOnvifDeviceComponent', () => {
  let component: AddAsOnvifDeviceComponent;
  let fixture: ComponentFixture<AddAsOnvifDeviceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AddAsOnvifDeviceComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AddAsOnvifDeviceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
