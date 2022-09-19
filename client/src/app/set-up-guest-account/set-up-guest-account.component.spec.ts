import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SetUpGuestAccountComponent } from './set-up-guest-account.component';

describe('SetUpGuestAccountComponent', () => {
  let component: SetUpGuestAccountComponent;
  let fixture: ComponentFixture<SetUpGuestAccountComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SetUpGuestAccountComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SetUpGuestAccountComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
