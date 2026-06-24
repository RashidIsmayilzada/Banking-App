<template>
  <div v-if="show" class="modal-backdrop">
    <div class="card modal-card">
      <div class="row" style="margin-bottom:24px">
        <h2 class="t-h3" style="margin:0">{{ isEdit ? 'Edit employee' : 'Add new employee' }}</h2>
        <span class="spacer" />
        <button class="btn btn--ghost btn--xs" style="padding:8px" @click="$emit('close')">
          <AppIcon name="x" :size="16" />
        </button>
      </div>

      <div v-if="error" class="banner banner--danger" style="margin-bottom:16px">
        <AppIcon name="alert" :size="16" class="banner__icon" />
        <div>{{ error }}</div>
      </div>

      <div class="col" style="gap:16px">
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
          <AppField label="First name" v-model="form.firstName" />
          <AppField label="Last name" v-model="form.lastName" />
        </div>

        <AppField label="Email" v-model="form.email" placeholder="jane.doe@inhollandbank.com" :disabled="isEdit" />

        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
          <AppField label="Username" v-model="form.username" placeholder="janedoe" :disabled="isEdit" />
          <AppField label="Employee Number" v-model="form.employeeNumber" placeholder="EMP-1042" :disabled="isEdit" />
        </div>

        <AppField v-if="!isEdit" label="Temporary Password" v-model="form.password" type="password" hint="Must be at least 8 characters" />
      </div>

      <div class="row" style="margin-top:32px;justify-content:flex-end;gap:12px">
        <button class="btn btn--ghost" :disabled="loading" @click="$emit('close')">Cancel</button>
        <button class="btn btn--primary" :disabled="loading" @click="submitForm">
          <span v-if="loading">Saving…</span>
          <template v-else>{{ isEdit ? 'Save changes' : 'Create employee' }}</template>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'
import * as adminService from '@/services/admin'

const props = defineProps({
  show: { type: Boolean, default: false },
  employee: { type: Object, default: null } // Catches the selected employee
})

const emit = defineEmits(['close', 'saved'])

const form = ref({
  firstName: '',
  lastName: '',
  email: '',
  username: '',
  employeeNumber: '',
  password: ''
})

const loading = ref(false)
const error = ref('')

// Compute if we are in edit mode based on the presence of the employee prop
const isEdit = computed(() => !!props.employee)

// Watch for changes to the selected employee and populate the form
watch(
    () => props.employee,
    (emp) => {
      if (emp) {
        // Edit mode: Fill the form
        form.value = {
          firstName: emp.firstName || '',
          lastName: emp.lastName || '',
          email: emp.email || '',
          username: emp.username || '',
          employeeNumber: emp.employeeNumber || '',
          password: '' // Keep empty
        }
      } else {
        // Create mode: Clear the form
        form.value = {
          firstName: '',
          lastName: '',
          email: '',
          username: '',
          employeeNumber: '',
          password: ''
        }
      }
    },
    { immediate: true }
)

async function submitForm() {
  error.value = ''
  loading.value = true

  try {
    if (isEdit.value) {
      // Send only the updatable fields to the backend
      await adminService.updateEmployee(props.employee.id, {
        firstName: form.value.firstName,
        lastName: form.value.lastName
      })
    } else {
      // Send the full payload to create a new employee
      await adminService.createEmployee(form.value)
    }

    // Clear form for next time
    form.value = { firstName: '', lastName: '', email: '', username: '', employeeNumber: '', password: '' }

    // Tell the parent view to close the modal and refresh the table
    emit('saved')
  } catch (err) {
    error.value = err.message || 'Failed to save employee.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(25, 28, 31, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
  padding: 20px;
}
.modal-card {
  width: 100%;
  max-width: 480px;
  background: #ffffff;
  box-shadow: 0 12px 32px rgba(0,0,0,0.1);
  max-height: 90vh;
  overflow-y: auto;
}
</style>