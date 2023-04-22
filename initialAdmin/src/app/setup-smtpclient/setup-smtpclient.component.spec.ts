import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SetupSMTPClientComponent } from './setup-smtpclient.component';

describe('SetupSMTPClientComponent', () => {
  let component: SetupSMTPClientComponent;
  let fixture: ComponentFixture<SetupSMTPClientComponent>;

  beforeEach(async () => {
    // @ts-ignore
    await TestBed.configureTestingModule({
      declarations: [ SetupSMTPClientComponent ]
    })
    .compileComponents();

    // @ts-ignore
    fixture = TestBed.createComponent(SetupSMTPClientComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
