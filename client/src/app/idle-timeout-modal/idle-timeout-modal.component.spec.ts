import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IdleTimeoutModalComponent } from './idle-timeout-modal.component';

describe('IdleTimeoutModalComponent', () => {
  let component: IdleTimeoutModalComponent;
  let fixture: ComponentFixture<IdleTimeoutModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ IdleTimeoutModalComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(IdleTimeoutModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
