import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CameraParamsComponent } from './camera-params.component';

describe('CameraParamsComponent', () => {
  let component: CameraParamsComponent;
  let fixture: ComponentFixture<CameraParamsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CameraParamsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CameraParamsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
