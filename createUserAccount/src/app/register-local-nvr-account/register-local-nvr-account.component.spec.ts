import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegisterLocalNvrAccountComponent } from './register-local-nvr-account.component';

describe('RegisterLocalNvrAccountComponent', () => {
  let component: RegisterLocalNvrAccountComponent;
  let fixture: ComponentFixture<RegisterLocalNvrAccountComponent>;

  beforeEach(async () => {
    // @ts-ignore
    await TestBed.configureTestingModule({
      declarations: [ RegisterLocalNvrAccountComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    // @ts-ignore
    fixture = TestBed.createComponent(RegisterLocalNvrAccountComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
