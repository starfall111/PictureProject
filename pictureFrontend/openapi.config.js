import { generateService } from '@umijs/openapi'

generateService({
  requestLibPath: "import request from '@/request'",
  schemaPath: 'http://localhost:4040/api/v3/api-docs',
  serversPath: './src',
})
