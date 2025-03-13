import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfirmCanDeactivateComponent } from './confirm-can-deactivate.component';

describe('ConfirmCanDeactivateComponent', () => {
  let component: ConfirmCanDeactivateComponent;
  let fixture: ComponentFixture<ConfirmCanDeactivateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmCanDeactivateComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConfirmCanDeactivateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
