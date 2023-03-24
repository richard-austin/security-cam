import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CameraAdminPageHostingComponent } from './camera-admin-page-hosting.component';

describe('CameraAdminPageHostingComponent', () => {
  let component: CameraAdminPageHostingComponent;
  let fixture: ComponentFixture<CameraAdminPageHostingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CameraAdminPageHostingComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CameraAdminPageHostingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
