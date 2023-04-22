import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateUserAccountContainerComponent } from './create-user-account-container.component';

describe('CreateUserAccountContainerComponent', () => {
  let component: CreateUserAccountContainerComponent;
  let fixture: ComponentFixture<CreateUserAccountContainerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CreateUserAccountContainerComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CreateUserAccountContainerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
