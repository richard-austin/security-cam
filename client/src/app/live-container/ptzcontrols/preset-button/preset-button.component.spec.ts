import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PresetButtonComponent } from './preset-button.component';

describe('PresetButtonComponent', () => {
  let component: PresetButtonComponent;
  let fixture: ComponentFixture<PresetButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PresetButtonComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PresetButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
