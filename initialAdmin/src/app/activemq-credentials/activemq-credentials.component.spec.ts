import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ActivemqCredentialsComponent } from './activemq-credentials.component';

describe('CloudCredentialsComponent', () => {
  let component: ActivemqCredentialsComponent;
  let fixture: ComponentFixture<ActivemqCredentialsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ActivemqCredentialsComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ActivemqCredentialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
