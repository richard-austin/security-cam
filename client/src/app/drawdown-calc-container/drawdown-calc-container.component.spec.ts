import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DrawdownCalcContainerComponent } from './drawdown-calc-container.component';

describe('DrawdownCalcContainerComponent', () => {
  let component: DrawdownCalcContainerComponent;
  let fixture: ComponentFixture<DrawdownCalcContainerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DrawdownCalcContainerComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DrawdownCalcContainerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
