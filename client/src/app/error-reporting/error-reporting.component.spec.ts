import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ErrorReportingComponent } from './error-reporting.component';

describe('ErrorReportingComponent', () => {
  let component: ErrorReportingComponent;
  let fixture: ComponentFixture<ErrorReportingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ErrorReportingComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ErrorReportingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
