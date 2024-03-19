import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OnvifFailuresComponent } from './onvif-failures.component';

describe('OnvifFailuresComponent', () => {
  let component: OnvifFailuresComponent;
  let fixture: ComponentFixture<OnvifFailuresComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ OnvifFailuresComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(OnvifFailuresComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
