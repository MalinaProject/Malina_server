export interface UserResponseDto {
  id: string;
  email: string;
  displayName: string;
  createdAt: string;
  updatedAt: string;
}

export const toUserResponse = (user: import('../../domain/user/user.aggregate').User): UserResponseDto => {
  const primitives = user.toPrimitives();
  return {
    id: primitives.id as string,
    email: primitives.email as string,
    displayName: primitives.displayName as string,
    createdAt: primitives.createdAt as string,
    updatedAt: primitives.updatedAt as string,
  };
};
