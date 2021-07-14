extern crate jni;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

#[no_mangle]
pub extern "system" fn Java_gregtech_api_util_RustUtility_hello(env: JNIEnv, _class: JClass, input: JString) -> jstring {
    let input: String = env.get_string(input)
            .expect("Couldn't get java string!").into();

    let output = env.new_string(format!("Hello, {}!", input))
            .expect("Couldn't create java string!");

    output.into_inner()
}
