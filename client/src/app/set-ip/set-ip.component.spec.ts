import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SetIpComponent } from './set-ip.component';

describe('SetIpComponent', () => {
  let component: SetIpComponent;
  let fixture: ComponentFixture<SetIpComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SetIpComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SetIpComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
