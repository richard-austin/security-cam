import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CredentialsForCameraAccessComponent } from './credentials-for-camera-access.component';

describe('CredentialsForCameraAccessComponent', () => {
  let component: CredentialsForCameraAccessComponent;
  let fixture: ComponentFixture<CredentialsForCameraAccessComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CredentialsForCameraAccessComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CredentialsForCameraAccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
