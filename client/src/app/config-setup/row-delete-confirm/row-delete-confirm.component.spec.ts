import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RowDeleteConfirmComponent } from './row-delete-confirm.component';

describe('RowDeleteConfirmComponent', () => {
  let component: RowDeleteConfirmComponent;
  let fixture: ComponentFixture<RowDeleteConfirmComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RowDeleteConfirmComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RowDeleteConfirmComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
