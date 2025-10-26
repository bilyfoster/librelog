from pydantic import BaseModel, EmailStr

class MagicLinkRequest(BaseModel):
    email: EmailStr

class Token(BaseModel):
    access_token: str
    token_type: str

class AdminLogin(BaseModel):
    email: EmailStr
    password: str

