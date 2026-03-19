const { test, expect } = require('@playwright/test');
const path = require('path');

test('cria sessao autenticada reutilizavel', async ({ page, context, baseURL }) => {
  const username = `pw${Date.now().toString().slice(-10)}`;
  const password = '123456';

  await page.goto(`${baseURL}/register`);
  await page.getByLabel('Nome de Usuário').fill(username);
  await page.getByLabel('Senha').fill(password);
  await page.getByRole('button', { name: 'Cadastrar' }).click();

  await expect(page).toHaveURL(/\/login/);
  await page.locator('input[name="username"]').fill(username);
  await page.locator('input[name="password"]').fill(password);
  await page.getByRole('button', { name: 'Entrar' }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await context.storageState({ path: path.join('playwright', '.auth', 'user.json') });
});
